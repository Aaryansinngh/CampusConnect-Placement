import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { recruiterApi, CreateDrivePayload } from '../../api/recruiter';
import { useAuth } from '../../context/AuthContext';
import { 
  Briefcase, 
  GraduationCap, 
  ListChecks, 
  ArrowRight, 
  ArrowLeft, 
  Check, 
  X,
  AlertCircle
} from 'lucide-react';

export const CreateDrivePage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [step, setStep] = useState<number>(1);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Form State
  const [jobTitle, setJobTitle] = useState('');
  const [jobDescription, setJobDescription] = useState('');
  const [salaryPackage, setSalaryPackage] = useState('');
  const [jobLocation, setJobLocation] = useState('Bangalore / Hyderabad, India');
  const [employmentType, setEmploymentType] = useState('FULL_TIME');
  const [vacancies, setVacancies] = useState<number>(5);
  const [applicationDeadline, setApplicationDeadline] = useState(
    new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16)
  );

  // Eligibility Criteria State
  const [minCgpa, setMinCgpa] = useState<number>(7.5);
  const [maxBacklogs, setMaxBacklogs] = useState<number>(0);
  const [selectedDepts, setSelectedDepts] = useState<string[]>([]);
  const [gradYears, setGradYears] = useState<number[]>([2026]);
  const [skillInput, setSkillInput] = useState('');
  const [requiredSkills, setRequiredSkills] = useState<string[]>(['Python', 'Data Structures', 'SQL']);

  // Fetch departments and companies
  const { data: departments = [] } = useQuery({
    queryKey: ['departments'],
    queryFn: () => recruiterApi.getDepartments(),
  });

  const { data: companies = [] } = useQuery({
    queryKey: ['companies'],
    queryFn: () => recruiterApi.getCompanies(),
  });

  const companyId = user?.recruiter?.company_id || (companies[0]?.id || '');

  const handleAddSkill = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && skillInput.trim()) {
      e.preventDefault();
      if (!requiredSkills.includes(skillInput.trim())) {
        setRequiredSkills([...requiredSkills, skillInput.trim()]);
      }
      setSkillInput('');
    }
  };

  const handleRemoveSkill = (skill: string) => {
    setRequiredSkills(requiredSkills.filter(s => s !== skill));
  };

  const toggleDept = (deptId: string) => {
    if (selectedDepts.includes(deptId)) {
      setSelectedDepts(selectedDepts.filter(id => id !== deptId));
    } else {
      setSelectedDepts([...selectedDepts, deptId]);
    }
  };

  const handleSubmit = async () => {
    setError(null);
    setLoading(true);

    try {
      const payload: CreateDrivePayload = {
        company_id: companyId,
        job_title: jobTitle,
        job_description: jobDescription,
        salary_package_ctc: salaryPackage,
        job_location: jobLocation,
        employment_type: employmentType,
        vacancies: vacancies,
        application_deadline: new Date(applicationDeadline).toISOString(),
        eligibility: {
          min_cgpa: minCgpa,
          max_backlogs: maxBacklogs,
          eligible_department_ids: selectedDepts.length > 0 ? selectedDepts : departments.map(d => d.id),
          eligible_graduation_years: gradYears,
          required_skills: requiredSkills,
          min_attendance_percentage: 75.0,
        },
      };

      await recruiterApi.createDrive(payload);
      navigate('/recruiter/drives');
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to publish placement drive. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Wizard Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Create Campus Recruitment Drive</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Define job specifications, eligibility criteria, and assessment parameters for university applicants.
        </p>
      </div>

      {/* Steps Progress Indicator */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
        {[
          { step: 1, label: 'Role & Compensation', icon: Briefcase },
          { step: 2, label: 'Eligibility Engine', icon: GraduationCap },
          { step: 3, label: 'Review & Publish', icon: ListChecks },
        ].map((item) => {
          const Icon = item.icon;
          const isDone = step > item.step;
          const isCurrent = step === item.step;

          return (
            <div key={item.step} className="flex items-center gap-3">
              <div
                className={`h-9 w-9 rounded-xl flex items-center justify-center font-bold text-sm transition ${
                  isDone
                    ? 'bg-emerald-500 text-white'
                    : isCurrent
                    ? 'bg-primary-600 text-white shadow-md shadow-primary-600/30'
                    : 'bg-slate-100 text-slate-400'
                }`}
              >
                {isDone ? <Check className="h-5 w-5" /> : <Icon className="h-4 w-4" />}
              </div>
              <div className="hidden sm:block">
                <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Step {item.step}</div>
                <div className={`text-xs font-semibold ${isCurrent ? 'text-slate-900' : 'text-slate-500'}`}>
                  {item.label}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-3 text-rose-700 text-sm">
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {/* Step 1: Job Role Details */}
      {step === 1 && (
        <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-sm space-y-6">
          <h2 className="text-lg font-bold text-slate-900 border-b border-slate-100 pb-3">
            Job Role &amp; Opportunity Specifications
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Job Designation / Title *
              </label>
              <input
                type="text"
                required
                value={jobTitle}
                onChange={(e) => setJobTitle(e.target.value)}
                placeholder="e.g. Associate Software Engineer, Cloud Infrastructure Intern"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Salary Package (CTC) *
              </label>
              <input
                type="text"
                required
                value={salaryPackage}
                onChange={(e) => setSalaryPackage(e.target.value)}
                placeholder="e.g. ₹ 28.5 LPA CTC or ₹ 80,000 / month"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Employment Type
              </label>
              <select
                value={employmentType}
                onChange={(e) => setEmploymentType(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm bg-white"
              >
                <option value="FULL_TIME">Full Time Employment</option>
                <option value="INTERNSHIP">Summer Internship</option>
                <option value="INTERN_PLUS_FTE">6-Month Internship + FTE</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Job Location(s) *
              </label>
              <input
                type="text"
                required
                value={jobLocation}
                onChange={(e) => setJobLocation(e.target.value)}
                placeholder="e.g. Bangalore, Hyderabad, Pune or Hybrid"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Target Vacancies / Headcount *
              </label>
              <input
                type="number"
                min="1"
                required
                value={vacancies}
                onChange={(e) => setVacancies(parseInt(e.target.value) || 1)}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Application Deadline *
              </label>
              <input
                type="datetime-local"
                required
                value={applicationDeadline}
                onChange={(e) => setApplicationDeadline(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Job Role Description &amp; Responsibilities *
              </label>
              <textarea
                rows={4}
                required
                value={jobDescription}
                onChange={(e) => setJobDescription(e.target.value)}
                placeholder="Detail key responsibilities, team structure, tech stack, and growth opportunities..."
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              />
            </div>
          </div>

          <div className="flex justify-end pt-4 border-t border-slate-100">
            <button
              type="button"
              disabled={!jobTitle || !salaryPackage || !jobDescription}
              onClick={() => setStep(2)}
              className="inline-flex items-center gap-2 px-6 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-xl text-sm transition disabled:opacity-50"
            >
              <span>Next: Eligibility Rules</span>
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 2: Eligibility Criteria */}
      {step === 2 && (
        <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-sm space-y-6">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Automated Eligibility Engine Rules</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              These criteria are evaluated server-side when students apply. Ineligible students cannot submit applications.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Minimum CGPA (out of 10.0) *
              </label>
              <input
                type="number"
                step="0.1"
                min="0"
                max="10"
                required
                value={minCgpa}
                onChange={(e) => setMinCgpa(parseFloat(e.target.value) || 0)}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm font-semibold"
              />
              <span className="text-xs text-slate-400 mt-1 block">Students below this CGPA will be automatically filtered out.</span>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Maximum Active Backlogs Permitted *
              </label>
              <select
                value={maxBacklogs}
                onChange={(e) => setMaxBacklogs(parseInt(e.target.value))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm bg-white"
              >
                <option value={0}>0 Backlogs (Strict Zero Arrear)</option>
                <option value={1}>Max 1 Active Backlog</option>
                <option value={2}>Max 2 Active Backlogs</option>
              </select>
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-2">
                Eligible Academic Departments
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {departments.map((dept) => {
                  const isChecked = selectedDepts.includes(dept.id);
                  return (
                    <button
                      key={dept.id}
                      type="button"
                      onClick={() => toggleDept(dept.id)}
                      className={`p-3 rounded-xl border text-left text-xs font-semibold transition flex items-center justify-between ${
                        isChecked
                          ? 'border-primary-500 bg-primary-50/50 text-primary-700'
                          : 'border-slate-200 bg-slate-50/50 text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <span>{dept.name}</span>
                      {isChecked && <Check className="h-4 w-4 text-primary-600" />}
                    </button>
                  );
                })}
              </div>
              <span className="text-xs text-slate-400 mt-1.5 block">
                Leave empty to allow all university engineering &amp; technology branches.
              </span>
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Target Graduation Batch
              </label>
              <div className="flex gap-3">
                {[2025, 2026, 2027].map((year) => {
                  const isSel = gradYears.includes(year);
                  return (
                    <button
                      key={year}
                      type="button"
                      onClick={() => {
                        if (isSel) setGradYears(gradYears.filter(y => y !== year));
                        else setGradYears([...gradYears, year]);
                      }}
                      className={`px-4 py-2 rounded-xl text-xs font-bold transition ${
                        isSel
                          ? 'bg-primary-600 text-white'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                      }`}
                    >
                      Class of {year}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Required Technical Skills (Press Enter to add)
              </label>
              <div className="p-3 border border-slate-200 rounded-xl focus-within:ring-2 focus-within:ring-primary-500 bg-white">
                <div className="flex flex-wrap gap-2 mb-2">
                  {requiredSkills.map((skill) => (
                    <span
                      key={skill}
                      className="inline-flex items-center gap-1.5 px-3 py-1 bg-primary-50 text-primary-700 text-xs font-semibold rounded-lg border border-primary-200"
                    >
                      {skill}
                      <button
                        type="button"
                        onClick={() => handleRemoveSkill(skill)}
                        className="hover:text-rose-600"
                      >
                        <X className="h-3.5 w-3.5" />
                      </button>
                    </span>
                  ))}
                </div>
                <input
                  type="text"
                  value={skillInput}
                  onChange={(e) => setSkillInput(e.target.value)}
                  onKeyDown={handleAddSkill}
                  placeholder="Type a skill and press Enter (e.g. Java, AWS, React, Docker)..."
                  className="w-full text-sm outline-none bg-transparent"
                />
              </div>
            </div>
          </div>

          <div className="flex justify-between pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={() => setStep(1)}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-xl text-sm transition"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Back</span>
            </button>

            <button
              type="button"
              onClick={() => setStep(3)}
              className="inline-flex items-center gap-2 px-6 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-xl text-sm transition"
            >
              <span>Next: Review &amp; Launch</span>
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 3: Review & Publish */}
      {step === 3 && (
        <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-sm space-y-6">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Placement Drive Review</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Confirm your recruitment parameters before publishing to university students.
            </p>
          </div>

          <div className="bg-slate-50 rounded-2xl p-6 border border-slate-200/80 space-y-4">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="text-xl font-extrabold text-slate-900">{jobTitle}</h3>
                <p className="text-xs font-semibold text-primary-600 uppercase mt-0.5">
                  {employmentType.replace('_', ' ')} • {vacancies} Open Vacancies
                </p>
              </div>
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-primary-600 text-white">
                {salaryPackage}
              </span>
            </div>

            <p className="text-sm text-slate-600 whitespace-pre-line leading-relaxed">
              {jobDescription}
            </p>

            <div className="pt-4 border-t border-slate-200/60 grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
              <div>
                <span className="text-slate-400 font-bold uppercase block text-[10px]">Min CGPA</span>
                <strong className="text-slate-800 text-sm">{minCgpa}</strong>
              </div>
              <div>
                <span className="text-slate-400 font-bold uppercase block text-[10px]">Max Backlogs</span>
                <strong className="text-slate-800 text-sm">{maxBacklogs}</strong>
              </div>
              <div>
                <span className="text-slate-400 font-bold uppercase block text-[10px]">Location</span>
                <strong className="text-slate-800 text-sm">{jobLocation}</strong>
              </div>
              <div>
                <span className="text-slate-400 font-bold uppercase block text-[10px]">Deadline</span>
                <strong className="text-slate-800 text-sm">{applicationDeadline.slice(0, 10)}</strong>
              </div>
            </div>

            {requiredSkills.length > 0 && (
              <div className="pt-3 border-t border-slate-200/60">
                <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1.5">Required Skills</span>
                <div className="flex flex-wrap gap-1.5">
                  {requiredSkills.map((s) => (
                    <span key={s} className="px-2.5 py-0.5 rounded-md bg-white border border-slate-200 text-slate-700 text-xs font-medium">
                      {s}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div className="flex justify-between pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={() => setStep(2)}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-xl text-sm transition"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Back</span>
            </button>

            <button
              type="button"
              disabled={loading}
              onClick={handleSubmit}
              className="inline-flex items-center gap-2 px-8 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-sm transition shadow-lg shadow-emerald-600/25 disabled:opacity-50"
            >
              <span>{loading ? 'Publishing Drive...' : 'Publish Drive to Campus'}</span>
              <Check className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
